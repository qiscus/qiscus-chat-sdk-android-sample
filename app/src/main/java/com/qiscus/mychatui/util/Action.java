package com.qiscus.mychatui.util;

/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public interface Action<T> {
    void call(T t);
}
