package com.bwtst.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

// add by codebuddy 20260817
public class CodeGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 前缀+日期 -> 当日已生成的最大流水号（内存兜底，防止并发重复） */
    private static final ConcurrentHashMap<String, AtomicInteger> SEQ_MAP = new ConcurrentHashMap<>();

    private CodeGenerator() {
    }

    /**
     * 生成单据编码：前缀 + 时间戳 + 2位随机数，例如 RQ2026081710305512
     * （仅用于要货模板等非业务单据）
     */
    public static String gen(String prefix) {
        int random = ThreadLocalRandom.current().nextInt(10, 99);
        return prefix + LocalDateTime.now().format(FMT) + random;
    }

    /**
     * 生成每日流水单据编码：前缀 + 年月日 + 三位流水号，例如 YHSQSH000120260817001
     * 前缀可包含门店编号（如 YHSQ + 门店编号），流水号当天从 001 开始自增，次日重新从 001 开始。
     *
     * @param prefix       单据前缀（如 YHSQ 或 YHSQ + 门店编号）
     * @param todayMaxSeq  数据库中当天已存在的最大流水号，无则为 0
     */
    public static String genDaily(String prefix, int todayMaxSeq) {
        String day = LocalDate.now().format(DAY_FMT);
        AtomicInteger counter = SEQ_MAP.computeIfAbsent(prefix + day, k -> new AtomicInteger(Math.max(todayMaxSeq, 0)));
        // 以数据库最大值和内存计数中的较大者作为起点，避免重启后重复
        counter.accumulateAndGet(Math.max(todayMaxSeq, 0), Math::max);
        int seq = counter.incrementAndGet();
        return prefix + day + String.format("%03d", seq);
    }
}
