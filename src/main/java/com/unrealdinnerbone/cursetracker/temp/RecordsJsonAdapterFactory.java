package com.unrealdinnerbone.cursetracker.temp;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public final class RecordsJsonAdapterFactory implements JsonAdapter.Factory {

    @Override
    public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
        return RecordJsonAdapter.FACTORY.create(type, annotations, moshi);
    }
}