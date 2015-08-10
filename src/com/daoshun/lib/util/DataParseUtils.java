package com.daoshun.lib.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * JSON处理工具
 */
public class DataParseUtils {

    /**
     * 取得所有域变量（包括父类）
     * 
     * @param cls
     *            类
     * @param end
     *            最终父类
     */
    public static List<Field> getFields(Class<?> cls, Class<?> end) {

        List<Field> list = new ArrayList<Field>();

        if (!cls.equals(end)) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                list.add(field);
            }

            Class<?> superClass = (Class<?>) cls.getGenericSuperclass();
            list.addAll(getFields(superClass, end));
        }

        return list;
    }

    /**
     * 解析JSON对象
     * 
     * @param jsonObject
     *            JSON对象
     * @param bean
     *            Bean
     */
    public static void parseJSONObject(JSONObject jsonObject, Object data) throws Exception {

        List<Field> fieldList = getFields(data.getClass(), Object.class);

        for (Field field : fieldList) {
            field.setAccessible(true);

            String fieldName = field.getName();

            if (jsonObject.has(fieldName)
                    && !jsonObject.isNull(fieldName)
                    && String.valueOf(jsonObject.get(fieldName)).length() > 0) {

                Class<?> type = field.getType();

                if (List.class.equals(type)) {
                    ParameterizedType pType = (ParameterizedType) field.getGenericType();
                    Class<?> clazz = (Class<?>) pType.getActualTypeArguments()[0];

                    JSONArray jsonArray = jsonObject.getJSONArray(fieldName);
                    ArrayList<Object> list = new ArrayList<Object>();

                    boolean isInnerClass = clazz.getName().contains("$");

                    for (int j = 0; j < jsonArray.length(); j++) {
                        Object childBean = null;
                        if (isInnerClass) {
                            childBean = clazz.getDeclaredConstructors()[0].newInstance(data);
                        } else {
                            childBean = clazz.newInstance();
                        }

                        JSONObject subJsonObject = jsonArray.optJSONObject(j);
                        if (subJsonObject == null) {
                            childBean = jsonArray.get(j);
                        } else {
                            parseJSONObject(subJsonObject, childBean);
                        }
                        list.add(childBean);
                    }
                    field.set(data, list);

                } else if (String.class.equals(type)) {
                    field.set(data, jsonObject.getString(fieldName));

                } else if (int.class.equals(type) || Integer.class.equals(type)) {
                    field.setInt(data, jsonObject.getInt(fieldName));

                } else if (float.class.equals(type) || Float.class.equals(type)) {
                    field.setFloat(data, (float) jsonObject.getDouble(fieldName));

                } else if (double.class.equals(type) || Double.class.equals(type)) {
                    field.setDouble(data, jsonObject.getDouble(fieldName));

                } else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
                    field.setBoolean(data, jsonObject.getBoolean(fieldName));

                } else if (long.class.equals(type) || Long.class.equals(type)) {
                    field.setLong(data, jsonObject.getLong(fieldName));

                } else {
                    JSONObject childObject = jsonObject.getJSONObject(fieldName);

                    boolean isInnerClass = type.getName().contains("$");

                    Object childBean = null;
                    if (isInnerClass) {
                        childBean = type.getDeclaredConstructors()[0].newInstance(data);
                    } else {
                        childBean = type.newInstance();
                    }

                    parseJSONObject(childObject, childBean);
                    field.set(data, childBean);
                }
            }
        }
    }
}
