package com.theembers.iot.router.route;


import com.theembers.iot.collector.SourceData;
import com.theembers.iot.processor.Output;
import com.theembers.iot.processor.Processor;
import com.theembers.iot.processor.SlotData;
import com.theembers.iot.shadow.Shadow;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * 处理器调度员
 *
 * @author TheEmbers Guo createTime 2019-11-14 10:43
 */
public class Dispatcher implements Iterable<Processor> {

    private Processor first;
    private Processor last;
    private int mark;
    private List<Processor> link;

    public Processor getFirst() {
        return first;
    }

    public Processor getLast() {
        return last;
    }

    public void setFirst(Processor processor) {
        this.first = processor;
    }

    public void setLast(Processor processor) {
        this.last = processor;
    }

    public void append(Processor processor) {
        if (first == null) {
            link = new LinkedList<>();
            setFirst(processor);
        }
        link.add(processor);
        setLast(processor);
        mark++;
    }

    public Processor getMark() {
        if (link.size() == 0) {
            return null;
        }
        return link.get(mark);
    }

    @Override
    public Iterator<Processor> iterator() {
        return link.listIterator();
    }

    @Override
    public void forEach(Consumer<? super Processor> action) {
        Objects.requireNonNull(action);
        for (Processor t : this) {
            action.accept(t);
        }
    }

    @Override
    public Spliterator<Processor> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }

    /**
     * 执行 如果 当前processor是头节点，调用 headIn （ 调用 beforeTransform & transForm） 否则 （中间节点 或者 尾节点） 调用 receive（接收） 最终 如果 是尾结点 则 调用 tailOut （调用 afterTransform） 并
     * 退出循环 到 //1 则 调用 buildSlotData （构建插槽）
     *
     * @param shadow
     * @param sourceData
     */
    void run(Shadow shadow, SourceData sourceData) {
        Output output = null;
        SlotData slotData = null;
        Iterator<Processor> processors = this.link.iterator();
        while (processors.hasNext()) {
            Processor p = processors.next();
            if (p == getFirst()) {
                output = p.headIn(shadow, sourceData);
            } else {
                output = p.receive(shadow, slotData);
            }
            if (p == getLast()) {
                output = p.tailOut(shadow, output);
                return;
            }
            slotData = p.passOn(shadow, output); // 1
        }
    }
}
