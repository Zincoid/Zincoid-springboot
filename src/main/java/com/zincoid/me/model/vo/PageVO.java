package com.zincoid.me.model.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

@Data
@Builder
public class PageVO<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;
    private long pages;

    public static <S, T> PageVO<T> of(Page<S> page, Function<S, T> mapper) {
        return PageVO.<T>builder()
                .records(page.getRecords().stream().map(mapper).toList())
                .total(page.getTotal())
                .page(page.getCurrent())
                .size(page.getSize())
                .pages(page.getPages())
                .build();
    }

    public static <T> PageVO<T> of(Page<T> page) {
        return PageVO.<T>builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .page(page.getCurrent())
                .size(page.getSize())
                .pages(page.getPages())
                .build();
    }

    public static <T> PageVO<T> of(IPage<?> page, List<T> records) {
        return PageVO.<T>builder()
                .records(records)
                .total(page.getTotal())
                .page(page.getCurrent())
                .size(page.getSize())
                .pages(page.getPages())
                .build();
    }
}
