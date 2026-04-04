package com.bbww.easyledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbww.easyledger.entity.Category;
import com.bbww.easyledger.entity.LedgerTransaction;
import com.bbww.easyledger.mapper.CategoryMapper;
import com.bbww.easyledger.mapper.LedgerTransactionMapper;
import com.bbww.easyledger.service.dto.StatsSummaryResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TransactionService {

    private enum PeriodType {
        DAY, WEEK, MONTH, YEAR
    }

    private static class PeriodBucket {
        private final LocalDate sortKey;
        private final String label;

        private PeriodBucket(LocalDate sortKey, String label) {
            this.sortKey = sortKey;
            this.label = label;
        }
    }

    private final LedgerTransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;

    public TransactionService(LedgerTransactionMapper transactionMapper, CategoryMapper categoryMapper) {
        this.transactionMapper = transactionMapper;
        this.categoryMapper = categoryMapper;
    }

    public void save(LedgerTransaction transaction) {
        transactionMapper.insert(transaction);
    }

    public LedgerTransaction findById(Long id) {
        return id == null ? null : transactionMapper.selectById(id);
    }

    public boolean updateById(Long id, LedgerTransaction transaction) {
        if (id == null || transaction == null || transactionMapper.selectById(id) == null) {
            return false;
        }
        transaction.setId(id);
        return transactionMapper.updateById(transaction) > 0;
    }

    public boolean deleteById(Long id) {
        return id != null && transactionMapper.deleteById(id) > 0;
    }

    public List<LedgerTransaction> findRecent(int limit) {
        int safeLimit = Math.max(1, limit);
        LambdaQueryWrapper<LedgerTransaction> query = new LambdaQueryWrapper<>();
        query.orderByDesc(LedgerTransaction::getTxnDate).orderByDesc(LedgerTransaction::getId).last("LIMIT " + safeLimit);
        return transactionMapper.selectList(query);
    }

    public List<LedgerTransaction> findAll() {
        LambdaQueryWrapper<LedgerTransaction> query = new LambdaQueryWrapper<>();
        query.orderByDesc(LedgerTransaction::getTxnDate).orderByDesc(LedgerTransaction::getId);
        return transactionMapper.selectList(query);
    }

    public List<LedgerTransaction> findByKeyword(String keyword) {
        String trimmed = keyword == null ? null : keyword.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return findAll();
        }
        LambdaQueryWrapper<LedgerTransaction> query = new LambdaQueryWrapper<>();
        query.like(LedgerTransaction::getNote, trimmed)
                .orderByDesc(LedgerTransaction::getTxnDate)
                .orderByDesc(LedgerTransaction::getId);
        return transactionMapper.selectList(query);
    }

    public List<LedgerTransaction> findByFilter(String type, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<LedgerTransaction> query = new LambdaQueryWrapper<>();
        query.eq(type != null && !type.isBlank(), LedgerTransaction::getType, type)
                .ge(startDate != null, LedgerTransaction::getTxnDate, startDate)
                .le(endDate != null, LedgerTransaction::getTxnDate, endDate)
                .orderByDesc(LedgerTransaction::getTxnDate)
                .orderByDesc(LedgerTransaction::getId);
        return transactionMapper.selectList(query);
    }

    public StatsSummaryResponse buildSummary(String type, LocalDate startDate, LocalDate endDate, String period) {
        PeriodType periodType = parsePeriod(period);
        List<LedgerTransaction> transactions = findByFilter(type, startDate, endDate);
        Map<Long, String> categoryNameMap = buildCategoryNameMap();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> periodTotals = new LinkedHashMap<>();
        Map<String, BigDecimal> categoryTotalsIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> categoryTotalsExpense = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> periodIncomeTotals = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> periodExpenseTotals = new LinkedHashMap<>();
        Map<LocalDate, String> periodLabels = new LinkedHashMap<>();

        for (LedgerTransaction transaction : transactions) {
            BigDecimal amount = safeAmount(transaction.getAmount());
            BigDecimal signedAmount = signedAmount(transaction.getType(), amount);

            if ("INCOME".equals(transaction.getType())) {
                totalIncome = totalIncome.add(amount);
            } else if ("EXPENSE".equals(transaction.getType())) {
                totalExpense = totalExpense.add(amount);
            }

            String categoryName = categoryNameMap.getOrDefault(transaction.getCategoryId(), "未分类");
            categoryTotals.put(categoryName, categoryTotals.getOrDefault(categoryName, BigDecimal.ZERO).add(signedAmount));

            PeriodBucket bucket = toPeriodBucket(transaction.getTxnDate(), periodType);
            periodLabels.putIfAbsent(bucket.sortKey, bucket.label);
            periodTotals.put(bucket.sortKey, periodTotals.getOrDefault(bucket.sortKey, BigDecimal.ZERO).add(signedAmount));

            if ("INCOME".equals(transaction.getType())) {
                categoryTotalsIncome.put(categoryName, categoryTotalsIncome.getOrDefault(categoryName, BigDecimal.ZERO).add(amount));
                periodIncomeTotals.put(bucket.sortKey, periodIncomeTotals.getOrDefault(bucket.sortKey, BigDecimal.ZERO).add(amount));
            } else if ("EXPENSE".equals(transaction.getType())) {
                categoryTotalsExpense.put(categoryName, categoryTotalsExpense.getOrDefault(categoryName, BigDecimal.ZERO).add(amount));
                periodExpenseTotals.put(bucket.sortKey, periodExpenseTotals.getOrDefault(bucket.sortKey, BigDecimal.ZERO).add(amount));
            }
        }

        List<LocalDate> sortedKeys = new ArrayList<>(periodLabels.keySet());
        sortedKeys.sort(Comparator.nullsLast(Comparator.naturalOrder()));

        List<StatsSummaryResponse.PeriodAmountItem> periodTotalItems = toPeriodItems(sortedKeys, periodLabels, periodTotals);
        List<StatsSummaryResponse.PeriodAmountItem> periodIncomeItems = toPeriodItems(sortedKeys, periodLabels, periodIncomeTotals);
        List<StatsSummaryResponse.PeriodAmountItem> periodExpenseItems = toPeriodItems(sortedKeys, periodLabels, periodExpenseTotals);

        StatsSummaryResponse response = new StatsSummaryResponse();
        response.setTotalIncome(totalIncome);
        response.setTotalExpense(totalExpense);
        response.setCategoryTotals(toCategoryItems(categoryTotals));
        response.setDailyTotals(toDailyItemsFromPeriodItems(periodTotalItems));
        response.setCategoryTotalsIncome(toCategoryItems(categoryTotalsIncome));
        response.setCategoryTotalsExpense(toCategoryItems(categoryTotalsExpense));
        response.setDailyIncomeTotals(toDailyItemsFromPeriodItems(periodIncomeItems));
        response.setDailyExpenseTotals(toDailyItemsFromPeriodItems(periodExpenseItems));
        response.setPeriodTotals(periodTotalItems);
        response.setPeriodIncomeTotals(periodIncomeItems);
        response.setPeriodExpenseTotals(periodExpenseItems);
        return response;
    }

    public Map<String, List<LedgerTransaction>> groupByPeriod(List<LedgerTransaction> transactions, String period) {
        PeriodType periodType = parsePeriod(period);
        Map<String, List<LedgerTransaction>> grouped = new LinkedHashMap<>();
        for (LedgerTransaction transaction : transactions) {
            PeriodBucket bucket = toPeriodBucket(transaction.getTxnDate(), periodType);
            grouped.computeIfAbsent(bucket.label, key -> new ArrayList<>()).add(transaction);
        }
        return grouped;
    }

    public String normalizePeriod(String period) {
        return parsePeriod(period).name().toLowerCase(Locale.ROOT);
    }

    private Map<Long, String> buildCategoryNameMap() {
        Map<Long, String> categoryNameMap = new LinkedHashMap<>();
        List<Category> categories = categoryMapper.selectList(null);
        for (Category category : categories) {
            categoryNameMap.put(category.getId(), category.getName());
        }
        return categoryNameMap;
    }

    private List<StatsSummaryResponse.CategoryAmountItem> toCategoryItems(Map<String, BigDecimal> categoryTotals) {
        List<StatsSummaryResponse.CategoryAmountItem> items = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            StatsSummaryResponse.CategoryAmountItem item = new StatsSummaryResponse.CategoryAmountItem();
            item.setCategoryName(entry.getKey());
            item.setAmount(entry.getValue());
            items.add(item);
        }
        return items;
    }

    private List<StatsSummaryResponse.DailyAmountItem> toDailyItems(Map<String, BigDecimal> dailyTotals) {
        List<StatsSummaryResponse.DailyAmountItem> items = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : dailyTotals.entrySet()) {
            StatsSummaryResponse.DailyAmountItem item = new StatsSummaryResponse.DailyAmountItem();
            item.setDay(entry.getKey());
            item.setAmount(entry.getValue());
            items.add(item);
        }
        return items;
    }

    private List<StatsSummaryResponse.PeriodAmountItem> toPeriodItems(List<LocalDate> sortedKeys,
                                                                      Map<LocalDate, String> labels,
                                                                      Map<LocalDate, BigDecimal> totals) {
        List<StatsSummaryResponse.PeriodAmountItem> items = new ArrayList<>();
        for (LocalDate key : sortedKeys) {
            StatsSummaryResponse.PeriodAmountItem item = new StatsSummaryResponse.PeriodAmountItem();
            item.setLabel(labels.get(key));
            item.setAmount(totals.getOrDefault(key, BigDecimal.ZERO));
            items.add(item);
        }
        return items;
    }

    private List<StatsSummaryResponse.DailyAmountItem> toDailyItemsFromPeriodItems(
            List<StatsSummaryResponse.PeriodAmountItem> periodItems) {
        List<StatsSummaryResponse.DailyAmountItem> items = new ArrayList<>();
        for (StatsSummaryResponse.PeriodAmountItem periodItem : periodItems) {
            StatsSummaryResponse.DailyAmountItem item = new StatsSummaryResponse.DailyAmountItem();
            item.setDay(periodItem.getLabel());
            item.setAmount(periodItem.getAmount());
            items.add(item);
        }
        return items;
    }

    private PeriodType parsePeriod(String period) {
        if (period == null || period.isBlank()) {
            return PeriodType.DAY;
        }
        switch (period.trim().toLowerCase(Locale.ROOT)) {
            case "week":
                return PeriodType.WEEK;
            case "month":
                return PeriodType.MONTH;
            case "year":
                return PeriodType.YEAR;
            default:
                return PeriodType.DAY;
        }
    }

    private PeriodBucket toPeriodBucket(LocalDate date, PeriodType periodType) {
        if (date == null) {
            return new PeriodBucket(null, "未知日期");
        }
        switch (periodType) {
            case WEEK:
                WeekFields weekFields = WeekFields.ISO;
                int week = date.get(weekFields.weekOfWeekBasedYear());
                int weekYear = date.get(weekFields.weekBasedYear());
                LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEnd = weekStart.plusDays(6);
                DateTimeFormatter md = DateTimeFormatter.ofPattern("MM-dd");
                String weekLabel = String.format("%d-W%02d (%s ~ %s)", weekYear, week,
                        weekStart.format(md), weekEnd.format(md));
                return new PeriodBucket(weekStart, weekLabel);
            case MONTH:
                YearMonth yearMonth = YearMonth.from(date);
                return new PeriodBucket(yearMonth.atDay(1), yearMonth.toString());
            case YEAR:
                LocalDate yearStart = LocalDate.of(date.getYear(), 1, 1);
                return new PeriodBucket(yearStart, String.valueOf(date.getYear()));
            case DAY:
            default:
                return new PeriodBucket(date, date.toString());
        }
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal signedAmount(String type, BigDecimal amount) {
        return "EXPENSE".equals(type) ? amount.negate() : amount;
    }
}
