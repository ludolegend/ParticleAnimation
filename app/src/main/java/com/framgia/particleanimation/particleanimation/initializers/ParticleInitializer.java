package com.framgia.particleanimation.particleanimation.initializers;

import com.framgia.particleanimation.particleanimation.Particle;

import java.util.Random;

public interface ParticleInitializer {

	void initParticle(Particle p, Random r);

}
