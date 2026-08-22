package com.zincoid.me.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zincoid.me.mapper.StatMapper;
import com.zincoid.me.model.po.Stat;
import com.zincoid.me.service.StatService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatServiceImpl extends ServiceImpl<StatMapper, Stat> implements StatService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();

    private final StatMapper statMapper;

    @Override
    public void record(String method, String path) {
        String key = LocalDate.now() + "|" + method + " " + path;
        counts.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    @PreDestroy
    public void shutdown() {
        flush();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void flush() {
        List<String> flushed = new ArrayList<>();
        for (Map.Entry<String, LongAdder> e : counts.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            try {
                statMapper.upsert(LocalDate.parse(parts[0], FMT), parts[1], e.getValue().sum());
                flushed.add(e.getKey());
            } catch (Exception ex) {
                log.warn("Failed to flush stat: {}", e.getKey(), ex);
            }
        }
        counts.keySet().removeAll(flushed);
    }

    @Override
    public Map<String, Object> stats(int days, int top) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);
        List<Stat> list = lambdaQuery().between(Stat::getStatDate, start, today).list();

        Map<String, Long> daily = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1))
            daily.put(d.toString(), 0L);
        Map<String, Long> apis = new LinkedHashMap<>();
        for (Stat u : list) {
            daily.merge(u.getStatDate().toString(), u.getCount(), Long::sum);
            apis.merge(u.getApi(), u.getCount(), Long::sum);
        }

        String prefix = today + "|";
        counts.forEach((k, v) -> {
            if (!k.startsWith(prefix)) return;
            String api = k.substring(prefix.length());
            long n = v.sum();
            daily.merge(today.toString(), n, Long::sum);
            apis.merge(api, n, Long::sum);
        });

        List<Map<String, Object>> dailyList = new ArrayList<>();
        daily.forEach((date, count) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", date);
            m.put("count", count);
            dailyList.add(m);
        });

        List<Map<String, Object>> apiList = new ArrayList<>();
        apis.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(Math.max(top, 0))
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("api", e.getKey());
                    m.put("count", e.getValue());
                    apiList.add(m);
                });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("daily", dailyList);
        result.put("apis", apiList);
        return result;
    }
}
