package com.eternalsnow.client;

import com.eternalsnow.state.EternalSnowState;

/** Tiny client entry point so shared code can tell "client present" from "dedicated server". */
public final class EternalSnowClientSetup {

    private EternalSnowClientSetup() {
    }

    public static void init() {
        EternalSnowState.markClientSide();
    }
}
