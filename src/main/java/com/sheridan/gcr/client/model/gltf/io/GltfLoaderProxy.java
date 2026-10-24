package com.sheridan.gcr.client.model.gltf.io;

import com.jme3.material.Material;
import com.jme3.scene.plugins.gltf.GltfLoader;

import java.lang.reflect.Field;

public class GltfLoaderProxy extends GltfLoader {
    public GltfLoaderProxy(){
        super();
        try {
            // 获取 GltfLoader 类中的 private 字段 "defaultMat"
            Field field = GltfLoader.class.getDeclaredField("defaultMat");
            field.setAccessible(true); // 设置字段可访问
            field.set(this, new Material()); // 设置默认材质
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 重写读取材质的方法，返回一个新的材质实例
    @Override
    public Material readMaterial(int materialIndex) {
        return new Material(); // 返回新创建的材质对象
    }
}


