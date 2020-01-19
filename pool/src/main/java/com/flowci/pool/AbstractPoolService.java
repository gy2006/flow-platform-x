package com.flowci.pool;

public abstract class AbstractPoolService<T extends PoolContext> implements PoolService<T>{

    protected static final String Image = "flowci/agent:latest";

    protected int max = 10;

    @Override
    public void setSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Max agent size must be positive integer");
        }
        max = size;
    }
    
}