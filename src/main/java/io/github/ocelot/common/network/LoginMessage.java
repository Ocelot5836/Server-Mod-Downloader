package io.github.ocelot.common.network;

import java.util.function.IntSupplier;

/**
 * @author Ocelot
 */
public abstract class LoginMessage implements IntSupplier
{
    private int loginIndex;

    public void setLoginIndex(int loginIndex)
    {
        this.loginIndex = loginIndex;
    }

    @Override
    public int getAsInt()
    {
        return loginIndex;
    }
}
