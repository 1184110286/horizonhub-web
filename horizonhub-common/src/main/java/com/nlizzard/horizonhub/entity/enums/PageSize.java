package com.nlizzard.horizonhub.entity.enums;

//分页的枚举类
public enum PageSize {
    // 定义枚举常量及其对应的值
    SIZE15(15), SIZE20(20), SIZE30(30), SIZE40(40), SIZE50(50);

    // 成员变量，表示每页的大小
    private final int size;

    // 枚举类的构造函数，接受一个参数size
    PageSize(int size) {
        this.size = size;
    }

    // 获取每页的大小
    public int getSize() {
        return this.size;
    }
}
