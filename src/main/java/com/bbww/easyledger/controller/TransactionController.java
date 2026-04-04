package com.bbww.easyledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbww.easyledger.entity.Category;
import com.bbww.easyledger.entity.LedgerTransaction;
import com.bbww.easyledger.mapper.CategoryMapper;
import com.bbww.easyledger.service.TransactionService;
import com.bbww.easyledger.service.dto.StatsSummaryResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Controller
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final CategoryMapper categoryMapper;

    public TransactionController(TransactionService transactionService, CategoryMapper categoryMapper) {
        this.transactionService = transactionService;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/new")
    public String form(Model model) {
        prepareForm(model, defaultTransaction(), null, false);
        return "transaction-form";
    }

    @PostMapping
    public String create(@ModelAttribute("transaction") LedgerTransaction transaction, Model model) {
        if (!isValid(transaction)) {
            prepareForm(model, transaction, "请填写完整信息，且金额必须大于0", false);
            return "transaction-form";
        }

        transactionService.save(transaction);
        return "redirect:/transactions";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model) {
        LedgerTransaction transaction = transactionService.findById(id);
        if (transaction == null) {
            return "redirect:/transactions";
        }
        prepareForm(model, transaction, null, true);
        return "transaction-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
                         @ModelAttribute("transaction") LedgerTransaction transaction,
                         Model model) {
        if (!isValid(transaction)) {
            prepareForm(model, transaction, "请填写完整信息，且金额必须大于0", true);
            return "transaction-form";
        }
        if (!transactionService.updateById(id, transaction)) {
            prepareForm(model, transaction, "未找到要更新的记录", true);
            return "transaction-form";
        }
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id) {
        transactionService.deleteById(id);
        return "redirect:/transactions";
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String period,
            Model model) {
        String normalizedPeriod = transactionService.normalizePeriod(normalizePeriodDefault(period));
        LocalDate[] range = resolveDateRange(startDate, endDate, normalizedPeriod);
        startDate = range[0];
        endDate = range[1];
        model.addAttribute("type", type);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("period", normalizedPeriod);
        model.addAttribute("recentTransactions", transactionService.findRecent(5));
        model.addAttribute("categoryNameMap", buildCategoryNameMap());
        return "dashboard";
    }

    @GetMapping("/records")
    public String records(@RequestParam(required = false) String period,
                          @RequestParam(required = false) String keyword,
                          Model model) {
        String normalizedPeriod = transactionService.normalizePeriod(period);
        List<LedgerTransaction> transactions = transactionService.findByKeyword(keyword);
        Map<String, List<LedgerTransaction>> grouped = transactionService.groupByPeriod(transactions, normalizedPeriod);
        Map<String, PeriodSubtotal> subtotals = buildGroupSubtotals(grouped);
        model.addAttribute("groupedTransactions", grouped);
        model.addAttribute("groupSubtotals", subtotals);
        model.addAttribute("categoryNameMap", buildCategoryNameMap());
        model.addAttribute("period", normalizedPeriod);
        model.addAttribute("periodLabel", periodLabel(normalizedPeriod));
        model.addAttribute("keyword", keyword);
        return "transaction-records";
    }

    @GetMapping("/stats/summary")
    @ResponseBody
    public StatsSummaryResponse statsSummary(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String period) {
        String normalizedPeriod = transactionService.normalizePeriod(normalizePeriodDefault(period));
        LocalDate[] range = resolveDateRange(startDate, endDate, normalizedPeriod);
        return transactionService.buildSummary(type, range[0], range[1], normalizedPeriod);
    }

    private LedgerTransaction defaultTransaction() {
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setTxnDate(LocalDate.now());
        transaction.setType("EXPENSE");
        return transaction;
    }

    private List<Category> loadCategories() {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<>();
        query.eq(Category::getEnabled, true).orderByAsc(Category::getType).orderByAsc(Category::getSort);
        return categoryMapper.selectList(query);
    }

    private Map<Long, String> buildCategoryNameMap() {
        return loadCategories().stream().collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private Map<String, PeriodSubtotal> buildGroupSubtotals(Map<String, List<LedgerTransaction>> grouped) {
        Map<String, PeriodSubtotal> subtotals = new LinkedHashMap<>();
        if (grouped == null) {
            return subtotals;
        }
        for (Map.Entry<String, List<LedgerTransaction>> entry : grouped.entrySet()) {
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            for (LedgerTransaction transaction : entry.getValue()) {
                if (transaction == null || transaction.getAmount() == null) {
                    continue;
                }
                if ("INCOME".equalsIgnoreCase(transaction.getType())) {
                    income = income.add(transaction.getAmount());
                } else if ("EXPENSE".equalsIgnoreCase(transaction.getType())) {
                    expense = expense.add(transaction.getAmount());
                }
            }
            subtotals.put(entry.getKey(), new PeriodSubtotal(income, expense));
        }
        return subtotals;
    }

    private static class PeriodSubtotal {
        private final BigDecimal income;
        private final BigDecimal expense;

        private PeriodSubtotal(BigDecimal income, BigDecimal expense) {
            this.income = income;
            this.expense = expense;
        }

        public BigDecimal getIncome() {
            return income;
        }

        public BigDecimal getExpense() {
            return expense;
        }
    }

    private boolean isValid(LedgerTransaction transaction) {
        return transaction.getAmount() != null
                && transaction.getAmount().signum() > 0
                && transaction.getTxnDate() != null
                && transaction.getCategoryId() != null
                && transaction.getType() != null
                && !transaction.getType().isBlank();
    }

    private void prepareForm(Model model, LedgerTransaction transaction, String error, boolean isEdit) {
        model.addAttribute("transaction", transaction);
        model.addAttribute("categories", loadCategories());
        model.addAttribute("error", error);
        model.addAttribute("formTitle", isEdit ? "编辑记账" : "新增记账");
        model.addAttribute("formSubtitle", isEdit ? "更新这笔记录的内容" : "快速记录一笔收入或支出");
        model.addAttribute("submitLabel", isEdit ? "保存修改" : "保存");
        String action = isEdit && transaction.getId() != null
                ? "/transactions/" + transaction.getId()
                : "/transactions";
        model.addAttribute("formAction", action);
    }

    private String periodLabel(String period) {
        if (period == null) {
            return "天";
        }
        switch (period) {
            case "week":
                return "周";
            case "month":
                return "月";
            case "year":
                return "年";
            default:
                return "天";
        }
    }

    private String normalizePeriodDefault(String period) {
        return (period == null || period.isBlank()) ? "month" : period;
    }

    private LocalDate[] resolveDateRange(LocalDate startDate, LocalDate endDate, String period) {
        if (startDate == null && endDate == null) {
            return currentRange(period, LocalDate.now());
        }
        if (startDate == null || endDate == null) {
            LocalDate anchor = startDate != null ? startDate : endDate;
            return currentRange(period, anchor);
        }
        return new LocalDate[]{startDate, endDate};
    }

    private LocalDate[] currentRange(String period, LocalDate anchor) {
        if (anchor == null) {
            anchor = LocalDate.now();
        }
        switch (period) {
            case "week":
                LocalDate weekStart = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                return new LocalDate[]{weekStart, weekStart.plusDays(6)};
            case "year":
                LocalDate yearStart = LocalDate.of(anchor.getYear(), 1, 1);
                return new LocalDate[]{yearStart, LocalDate.of(anchor.getYear(), 12, 31)};
            case "month":
                YearMonth month = YearMonth.from(anchor);
                return new LocalDate[]{month.atDay(1), month.atEndOfMonth()};
            case "day":
            default:
                return new LocalDate[]{anchor, anchor};
        }
    }
}
