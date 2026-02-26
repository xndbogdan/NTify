package com.spotifyxp.args;

import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;

import java.io.File;

public class Portable implements Argument {
    @Override
    public Runnable runArgument(String parameter1) {
        return () -> {
            PublicValues.foundSetupArgument = true;
            PublicValues.fileslocation = "ntify-data";
            PublicValues.configfilepath = PublicValues.fileslocation + File.separator + "config.json";
            PublicValues.customSaveDir = true;
            new File(PublicValues.fileslocation).mkdirs();
            ConsoleLogging.info("Portable mode: using local directory 'ntify-data'");
        };
    }

    @Override
    public String getName() {
        return "portable";
    }

    @Override
    public String getDescription() {
        return "Run in portable mode (stores config in local ntify-data folder, skips setup)";
    }

    @Override
    public boolean hasParameter() {
        return false;
    }
}
