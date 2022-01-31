package com.unrealdinnerbone.cursetracker.temp;

import com.squareup.moshi.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;

import static java.lang.invoke.MethodType.methodType;

final class RecordJsonAdapter<T> extends JsonAdapter<T> {

    static final Factory FACTORY =
            (type, annotations, moshi) -> {
                if (!annotations.isEmpty()) {
                    return null;
                }

                if (!(type instanceof Class) && !(type instanceof ParameterizedType)) {
                    return null;
                }

                var rawType = Types.getRawType(type);
                if (!rawType.isRecord()) {
                    return null;
                }

                Map<String, Type> mappedTypeArgs = null;
                if (type instanceof ParameterizedType parameterizedType) {
                    Type[] typeArgs = parameterizedType.getActualTypeArguments();
                    var typeVars = rawType.getTypeParameters();
                    mappedTypeArgs = new LinkedHashMap<>(typeArgs.length);
                    for (int i = 0; i < typeArgs.length; ++i) {
                        var typeVarName = typeVars[i].getName();
                        var materialized = typeArgs[i];
                        mappedTypeArgs.put(typeVarName, materialized);
                    }
                }
                var components = rawType.getRecordComponents();
                var bindings = new LinkedHashMap<String, ComponentBinding<?>>();
                var constructorParams = new Class<?>[components.length];
                var lookup = MethodHandles.lookup();
                for (int i = 0, componentsLength = components.length; i < componentsLength; i++) {
                    RecordComponent component = components[i];
                    constructorParams[i] = component.getType();
                    var name = component.getName();
                    var componentType = component.getGenericType();
                    if (componentType instanceof TypeVariable<?> typeVariable) {
                        var typeVarName = typeVariable.getName();
                        if (mappedTypeArgs == null) {
                            throw new AssertionError(
                                    "No mapped type arguments found for type '" + typeVarName + "'");
                        }
                        var mappedType = mappedTypeArgs.get(typeVarName);
                        if (mappedType == null) {
                            throw new AssertionError(
                                    "No materialized type argument found for type '" + typeVarName + "'");
                        }
                        componentType = mappedType;
                    }
                    var jsonName = name;
                    Set<Annotation> qualifiers = null;
                    for (var annotation : component.getDeclaredAnnotations()) {
                        if (annotation instanceof Json jsonAnnotation) {
                            jsonName = jsonAnnotation.name();
                        } else {
                            if (annotation.annotationType().isAnnotationPresent(JsonQualifier.class)) {
                                if (qualifiers == null) {
                                    qualifiers = new LinkedHashSet<>();
                                }
                                qualifiers.add(annotation);
                            }
                        }
                    }
                    if (qualifiers == null) {
                        qualifiers = Collections.emptySet();
                    }
                    var adapter = moshi.adapter(componentType, qualifiers);
                    MethodHandle accessor;
                    try {
                        accessor = lookup.unreflect(component.getAccessor());
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                    var componentBinding = new ComponentBinding<>(name, jsonName, adapter, accessor);
                    var replaced = bindings.put(jsonName, componentBinding);
                    if (replaced != null) {
                        throw new IllegalArgumentException(
                                "Conflicting components:\n"
                                        + "    "
                                        + replaced.name
                                        + "\n"
                                        + "    "
                                        + componentBinding.name);
                    }
                }

                MethodHandle constructor;
                try {
                    constructor = lookup.findConstructor(rawType, methodType(void.class, constructorParams));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }
                return new RecordJsonAdapter<>(constructor, rawType.getSimpleName(), bindings).nullSafe();
            };

    private static record ComponentBinding<T>(
            String name, String jsonName, JsonAdapter<T> adapter, MethodHandle accessor) {}

    private final String targetClass;
    private final MethodHandle constructor;
    private final ComponentBinding<Object>[] componentBindingsArray;
    private final JsonReader.Options options;

    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    public RecordJsonAdapter(
            MethodHandle constructor,
            String targetClass,
            Map<String, ComponentBinding<?>> componentBindings) {
        this.constructor = constructor;
        this.targetClass = targetClass;
        //noinspection unchecked
        this.componentBindingsArray =
                componentBindings.values().toArray(new ComponentBinding[componentBindings.size()]);
        this.options =
                JsonReader.Options.of(
                        componentBindings.keySet().toArray(new String[componentBindings.size()]));
    }

    @Override
    public T fromJson(JsonReader reader) throws IOException {
        var resultsArray = new Object[componentBindingsArray.length];

        reader.beginObject();
        while (reader.hasNext()) {
            int index = reader.selectName(options);
            if (index == -1) {
                reader.skipName();
                reader.skipValue();
                continue;
            }
            var result = componentBindingsArray[index].adapter.fromJson(reader);
            resultsArray[index] = result;
        }
        reader.endObject();

        try {
            //noinspection unchecked
            return (T) constructor.invokeWithArguments(resultsArray);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw new RuntimeException(cause);
            } else {
                throw new AssertionError(e);
            }
        }
    }

    @Override
    public void toJson(JsonWriter writer, T value) throws IOException {
        writer.beginObject();

        for (var binding : componentBindingsArray) {
            writer.name(binding.jsonName);
            try {
                binding.adapter.toJson(writer, binding.accessor.invoke(value));
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException ite) {
                    Throwable cause = ite.getCause();
                    if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                    if (cause instanceof Error) throw (Error) cause;
                    throw new RuntimeException(cause);
                } else {
                    throw new AssertionError(e);
                }
            }
        }

        writer.endObject();
    }

    @Override
    public String toString() {
        return "JsonAdapter(" + targetClass + ")";
    }
}