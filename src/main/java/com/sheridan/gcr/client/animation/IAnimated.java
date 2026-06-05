package com.sheridan.gcr.client.animation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public interface IAnimated {

    void offsetPos(Vector3f vector3f);

    void offsetRotation(Vector3f vector3f);

    void offsetScale(Vector3f vector3f);

    Optional<IAnimated> findByName(String pName);
}
