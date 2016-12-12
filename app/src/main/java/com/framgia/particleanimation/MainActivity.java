package com.framgia.particleanimation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.framgia.particleanimation.particleanimation.ParticleSystem;

public class MainActivity extends AppCompatActivity {

    private ParticleSystem mParticleSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void show(View view) {
        mParticleSystem = new ParticleSystem(this, 180, 12000)
                .setSpeedModuleAndAngleRange(0f, 0.14f, 0, 180)
                .setRotationSpeed(144)
                .setAcceleration(0.00008f, 90);
        mParticleSystem.emit(findViewById(R.id.toolbar), 20);
    }

    public void stop(View view) {
        mParticleSystem.stopEmitting();
    }
}
