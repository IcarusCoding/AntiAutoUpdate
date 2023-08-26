package de.intelligence.antiautoupdate.persistence;

@FunctionalInterface
public interface ExceptionFunction<T, E, R extends Exception> {

    E apply(T t) throws R;

}
