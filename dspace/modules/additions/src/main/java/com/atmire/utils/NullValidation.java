package com.atmire.utils;

import com.google.common.base.Function;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 09 Jan 2018
 *
 * Test if variables are null and accumulates the error messages.
 */
public class NullValidation<T> {
    private final String myCharacters;
    private final T previousObject;

    public NullValidation() {
        this("");
    }

    public NullValidation(String characters) {
        this(characters, null);
    }

    public NullValidation(String characters, T previous) {
        this.myCharacters = characters;
        this.previousObject = previous;
    }

    public boolean isOK() {
        return StringUtils.isBlank(myCharacters);
    }

    public String getCharacters() {
        return myCharacters;
    }

    public T getObject() {
        return previousObject;
    }

    private String notNull(Object object, String characters) {
        String newCharacters = myCharacters;
        if (object == null) {
            if (myCharacters.length() != 0) {
                characters += (" ");
            }
            newCharacters += characters;
        }
        return newCharacters;
    }

    public <R> NullValidation<R> take(R object, String characters) {
        return new NullValidation<>(notNull(object, characters), object);
    }

    public NullValidation<T> discard(Object object, String characters) {
        return new NullValidation<>(notNull(object, characters), previousObject);
    }

    public <R> NullValidation<R> thenTake(Function<T, R> function, String characters) {
        return previousObject != null ?
                take(function.apply(previousObject), characters) :
                take((R) null, "");
    }

    public NullValidation<T> thenDiscard(Function<T, Object> function, String characters) {
        return previousObject != null ?
                discard(function.apply(previousObject), characters) :
                this;
    }

}