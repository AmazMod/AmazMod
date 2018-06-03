package com.huami.watch.companion;


/**
 * Created by edoardotassinari on 27/02/18.
 */

/*
@DexAdd
public class RxConsumerStarter {


    public void init(CompanionApplication companionApplication) {
        RxBus.get().toObservable().subscribe(new RxConsumer(companionApplication));
    }

    @DexAdd
    private class RxConsumer implements Consumer<Object> {

        private CompanionApplication companionApplication;

        public RxConsumer(CompanionApplication companionApplication) {
            this.companionApplication = companionApplication;
        }

        @Override
        public void accept(Object object) {
            if (object instanceof DeviceUnboundEvent) {
                DeviceUnboundEvent event = (DeviceUnboundEvent) object;
                companionApplication.a(companionApplication, event.did, event.address, event.isActive);
            }
        }
    }
}
*/
