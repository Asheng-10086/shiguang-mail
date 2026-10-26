package com.example.demo.pojo;

import lombok.Data;

@Data
public class Result {
    
    private int Num;//返回的状态码 0代表成功，1代表失败
    private String msg; //返回的信息
    private Object data; // 返回的数据

    public Result(int Num,String msg)
    {
        this.Num = Num;
        this.msg = msg;
        this.data = null;
    }

    public Result(int Num,String msg,Object data)
    {
        this.Num = Num;
        this.msg = msg;
        this.data = data;
    }
    
    public static Result success()
    {
        return new Result(0,"success",null);
    }

    public static Result success(Object data1)
    {
        return new Result(0,"success",data1);
    }

    public static Result error(String errorMsg)
    {
        return new Result(1, errorMsg,null);
    }
}
