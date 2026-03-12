package com.bbww.easyledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bbww.easyledger.entity.LedgerTransaction;
import com.bbww.easyledger.mapper.LedgerTransactionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final LedgerTransactionMapper transactionMapper;

    public TransactionService(LedgerTransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
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
}
