package amazmod.com.transport;

import android.os.Bundle;

import com.huami.watch.transport.DataBundle;

public abstract class Transportable {

    public abstract void toDataBundle(DataBundle dataBundle);

    public abstract Bundle toBundle();

}
