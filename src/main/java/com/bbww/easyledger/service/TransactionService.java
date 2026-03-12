package com.bbww.easyledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbww.easyledger.entity.Category;
import com.bbww.easyledger.entity.LedgerTransaction;
import com.bbww.easyledger.mapper.CategoryMapper;
import com.bbww.easyledger.mapper.LedgerTransactionMapper;
import com.bbww.easyledger.service.dto.StatsSummaryResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final LedgerTransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;

    public TransactionService(LedgerTransactionMapper transactionMapper, CategoryMapper categoryMapper) {
        this.transactionMapper = transactionMapper;
        this.categoryMapper = categoryMapper;
    }

    public void save(LedgerTransaction transaction) {
        transactionMapper.insert(transaction);
    }

    public List<LedgerTransaction> findAll() {
        LambdaQueryWrapper<LedgerTransaction> query = new LambdaQueryWrapper<>();
        query.orderByDesc(LedgerTransaction::getTxnDate).orderByDesc(LedgerTransaction::getId);
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

    public StatsSummaryResponse buildSummary(String type, LocalDate startDate, LocalDate endDate) {
        List<LedgerTransaction> transactions = findByFilter(type, startDate, endDate);
        Map<Long, String> categoryNameMap = buildCategoryNameMap();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        Map<String, BigDecimal> dailyTotals = new LinkedHashMap<>();

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

            String day = transaction.getTxnDate() == null ? "未知日期" : transaction.getTxnDate().toString();
            dailyTotals.put(day, dailyTotals.getOrDefault(day, BigDecimal.ZERO).add(signedAmount));
        }

        StatsSummaryResponse response = new StatsSummaryResponse();
        response.setTotalIncome(totalIncome);
        response.setTotalExpense(totalExpense);
        response.setCategoryTotals(toCategoryItems(categoryTotals));
        response.setDailyTotals(toDailyItems(dailyTotals));
        return response;
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

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal signedAmount(String type, BigDecimal amount) {
        return "EXPENSE".equals(type) ? amount.negate() : amount;
    }
}
