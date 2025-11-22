package ch.eneas.flips.loader.util;

import ch.eneas.flips.loader.api.ClassProcessStage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.function.Consumer;
import java.util.function.Predicate;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public class SimpleClassProcessStage<T> implements ClassProcessStage<T> {
    String stageDescriptor;
    Predicate<Class<?>> filter;
    Consumer<ClassProcessInfo<T>> processor;

    @Override
    public String getStageDescriptor() {
        return stageDescriptor;
    }

    @Override
    public boolean matchesClass(Class<?> clazz) {
        return filter.test(clazz);
    }

    @Override
    public void processClass(ClassProcessInfo<T> info) {
        processor.accept(info);
    }

    @Override
    public String toString() {
        return stageDescriptor + " stage";
    }
}
