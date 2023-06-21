package com.exchange.core;

import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>(List.of("a","b","c"));
        for (String s: list){
            if(s.equals("a")){
                list.remove(s);
            }
        }
        System.out.println(list);
    }
}