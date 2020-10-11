package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.reflect.TypeToken;

public final class JsonAdapterAnnotationTypeAdapterFactory implements TypeAdapterFactory {
    private final ConstructorConstructor constructorConstructor;

    public JsonAdapterAnnotationTypeAdapterFactory(ConstructorConstructor constructorConstructor2) {
        this.constructorConstructor = constructorConstructor2;
    }

    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> targetType) {
        JsonAdapter annotation = (JsonAdapter) targetType.getRawType().getAnnotation(JsonAdapter.class);
        if (annotation == null) {
            return null;
        }
        return getTypeAdapter(this.constructorConstructor, gson, targetType, annotation);
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v14, resolved type: com.google.gson.TypeAdapter<?>} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v16, resolved type: com.google.gson.TypeAdapter} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r3v1, resolved type: com.google.gson.internal.bind.TreeTypeAdapter} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v17, resolved type: com.google.gson.internal.bind.TreeTypeAdapter} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v18, resolved type: com.google.gson.internal.bind.TreeTypeAdapter} */
    /* JADX WARNING: type inference failed for: r1v1, types: [com.google.gson.TypeAdapter<?>, com.google.gson.TypeAdapter] */
    /* access modifiers changed from: package-private */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public com.google.gson.TypeAdapter<?> getTypeAdapter(com.google.gson.internal.ConstructorConstructor r10, com.google.gson.Gson r11, com.google.gson.reflect.TypeToken<?> r12, com.google.gson.annotations.JsonAdapter r13) {
        /*
            r9 = this;
            java.lang.Class r0 = r13.value()
            com.google.gson.reflect.TypeToken r0 = com.google.gson.reflect.TypeToken.get(r0)
            com.google.gson.internal.ObjectConstructor r0 = r10.get(r0)
            java.lang.Object r0 = r0.construct()
            boolean r1 = r0 instanceof com.google.gson.TypeAdapter
            if (r1 == 0) goto L_0x0018
            r1 = r0
            com.google.gson.TypeAdapter r1 = (com.google.gson.TypeAdapter) r1
            goto L_0x0052
        L_0x0018:
            boolean r1 = r0 instanceof com.google.gson.TypeAdapterFactory
            if (r1 == 0) goto L_0x0024
            r1 = r0
            com.google.gson.TypeAdapterFactory r1 = (com.google.gson.TypeAdapterFactory) r1
            com.google.gson.TypeAdapter r1 = r1.create(r11, r12)
            goto L_0x0052
        L_0x0024:
            boolean r1 = r0 instanceof com.google.gson.JsonSerializer
            if (r1 != 0) goto L_0x0035
            boolean r1 = r0 instanceof com.google.gson.JsonDeserializer
            if (r1 == 0) goto L_0x002d
            goto L_0x0035
        L_0x002d:
            java.lang.IllegalArgumentException r1 = new java.lang.IllegalArgumentException
            java.lang.String r2 = "@JsonAdapter value must be TypeAdapter, TypeAdapterFactory, JsonSerializer or JsonDeserializer reference."
            r1.<init>(r2)
            throw r1
        L_0x0035:
            boolean r1 = r0 instanceof com.google.gson.JsonSerializer
            r2 = 0
            if (r1 == 0) goto L_0x003f
            r1 = r0
            com.google.gson.JsonSerializer r1 = (com.google.gson.JsonSerializer) r1
            r4 = r1
            goto L_0x0040
        L_0x003f:
            r4 = r2
        L_0x0040:
            boolean r1 = r0 instanceof com.google.gson.JsonDeserializer
            if (r1 == 0) goto L_0x0047
            r2 = r0
            com.google.gson.JsonDeserializer r2 = (com.google.gson.JsonDeserializer) r2
        L_0x0047:
            r5 = r2
            com.google.gson.internal.bind.TreeTypeAdapter r1 = new com.google.gson.internal.bind.TreeTypeAdapter
            r8 = 0
            r3 = r1
            r6 = r11
            r7 = r12
            r3.<init>(r4, r5, r6, r7, r8)
        L_0x0052:
            if (r1 == 0) goto L_0x005e
            boolean r2 = r13.nullSafe()
            if (r2 == 0) goto L_0x005e
            com.google.gson.TypeAdapter r1 = r1.nullSafe()
        L_0x005e:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(com.google.gson.internal.ConstructorConstructor, com.google.gson.Gson, com.google.gson.reflect.TypeToken, com.google.gson.annotations.JsonAdapter):com.google.gson.TypeAdapter");
    }
}
