package com.atmire.scripts;

import com.atmire.utils.Consumer;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 05 Jul 2016
 */
public class PrintConsumer {

    private final Consumer<String> info;
    private final Consumer<String> verbose;

    public PrintConsumer(Consumer<String> info, Consumer<String> verbose) {
        this.info = info;
        this.verbose = verbose;
    }

    public void info(String message) {
        this.info.consume(message);
    }

    public void verbose(String message) {
        this.verbose.consume(message);
    }
}
