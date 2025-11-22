package ch.eneas.flips.loader.util;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ConsumingClassProcessStage<T> extends SimpleClassProcessStage<T> {
    public ConsumingClassProcessStage(String stageDescriptor, Predicate<Class<?>> filter, Consumer<T> consumer) {
        super(stageDescriptor, filter, info -> consumer.accept(info.getProcessInstance()));
    }
}
