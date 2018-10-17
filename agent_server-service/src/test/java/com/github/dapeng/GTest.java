package com.github.dapeng;

import com.github.dapeng.socket.entity.DependServiceVo;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class GTest {

    public static void main(String[] args) {
        List<DependServiceVo> s = new ArrayList<>();

        Gson gson = new Gson();

        String json = gson.toJson(s);
        System.out.println(json);

        List<DependServiceVo> r = gson.fromJson(json, List.class);
        System.out.println(r);
    }
}
