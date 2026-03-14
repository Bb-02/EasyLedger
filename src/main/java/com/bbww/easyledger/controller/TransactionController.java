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
            Model model) {
        model.addAttribute("type", type);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("recentTransactions", transactionService.findRecent(5));
        model.addAttribute("categoryNameMap", buildCategoryNameMap());
        return "dashboard";
    }

    @GetMapping("/records")
    public String records(Model model) {
        List<LedgerTransaction> transactions = transactionService.findAll();
        Map<LocalDate, List<LedgerTransaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(LedgerTransaction::getTxnDate, java.util.LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("groupedTransactions", grouped);
        model.addAttribute("categoryNameMap", buildCategoryNameMap());
        return "transaction-records";
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
}
