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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        model.addAttribute("transaction", defaultTransaction());
        model.addAttribute("categories", loadCategories());
        return "transaction-form";
    }

    @PostMapping
    public String create(@ModelAttribute("transaction") LedgerTransaction transaction, Model model) {
        if (!isValid(transaction)) {
            model.addAttribute("transaction", transaction);
            model.addAttribute("error", "请填写完整信息，且金额必须大于0");
            model.addAttribute("categories", loadCategories());
            return "transaction-form";
        }

        transactionService.save(transaction);
        return "redirect:/transactions";
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        model.addAttribute("type", type);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "dashboard";
    }

    @GetMapping("/stats/summary")
    @ResponseBody
    public StatsSummaryResponse statsSummary(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionService.buildSummary(type, startDate, endDate);
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

    private boolean isValid(LedgerTransaction transaction) {
        return transaction.getAmount() != null
                && transaction.getAmount().signum() > 0
                && transaction.getTxnDate() != null
                && transaction.getCategoryId() != null
                && transaction.getType() != null
                && !transaction.getType().isBlank();
    }
}
