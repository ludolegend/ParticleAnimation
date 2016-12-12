package com.framgia.particleanimation.particleanimation.modifiers;

import com.framgia.particleanimation.particleanimation.Particle;

public interface ParticleModifier {

	/**
	 * modifies the specific value of a particle given the current miliseconds
	 * @param particle
	 * @param miliseconds
	 */
	void apply(Particle particle, long miliseconds);

}
