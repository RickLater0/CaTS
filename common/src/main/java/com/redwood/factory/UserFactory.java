package com.redwood.factory;

import com.redwood.dataobj.IntegerType;
import com.redwood.dataobj.StringType;
import com.redwood.dataobj.VectorPolar;
import com.redwood.interfaces.UserType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserFactory {

    private final Map<String, UserType> dataTypes;
    private static final List<UserType> types = List.of(
            new VectorPolar(),
            new StringType(),
            new IntegerType()
    );

    private final List<String> typeNames = new ArrayList<>();

    public UserFactory(){
        dataTypes = new HashMap<>();
        for(var type : types){
            typeNames.add(type.typeName());
            dataTypes.put(type.typeName(), type);
        }
    }

    public List<String> getTypeNameList(){
        return typeNames;
    }
    public UserType getBuilderByName(String name){
        return dataTypes.get(name);
    }
}
