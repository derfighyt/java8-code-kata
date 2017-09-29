package stream.api;

import com.sun.deploy.util.StringUtils;
import common.test.tool.annotation.Necessity;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.util.CollectorImpl;
import org.junit.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class Exercise9Test extends ClassicOnlineStore {

    @Test
    @Necessity(true)
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        //使用StringBuilder实现
        //Supplier<StringBuilder> supplier = StringBuilder::new; //指定collector最后返回的容器创建方法
        //BiConsumer<StringBuilder, String> accumulator = (container, str) -> container.append(str).append(","); //指定元素如何加入到容器里
        //BinaryOperator<StringBuilder> combiner = StringBuilder::append; //指定两个容器如何合并
        //Function<StringBuilder, String> finisher = container ->
        //        container.deleteCharAt(container.length() - 1).toString(); //指定对容器最后需要进行怎样的处理以返回需要的结果，可选

        //使用List实现
        Supplier<List<String>> supplier = ArrayList::new; //指定collector最后返回的容器创建方法
        BiConsumer<List<String>, String> accumulator = List::add; //指定元素如何加入到容器里
        BinaryOperator<List<String>> combiner = (c1, c2) -> {c1.addAll(c2); return c1;}; //指定两个容器如何合并
        Function<List<String>, String> finisher = container -> StringUtils.join(container, ","); //指定对容器最后需要进行怎样的处理以返回需要的结果，可选

        Collector<String, ?, String> toCsv =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Test
    @Necessity(false)
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Map<String, Set<String>>> supplier = HashMap::new;
        BiConsumer<Map<String, Set<String>>, Customer> accumulator = (map, customer) -> {
            customer.getWantToBuy().stream()
                    .forEach(item -> {
                        Set<String> set = Stream.of(customer.getName()).collect(Collectors.toSet());
                        map.merge(item.getName(), set, (o, n) -> {o.addAll(n); return o;});
                    });
        };
        BinaryOperator<Map<String, Set<String>>> combiner = (map1, map2) -> {
            map2.forEach((key, set) -> map1.merge(key, set, (o, n) -> {o.addAll(n);return o;}));
            return map1;
        };
        Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher = null;

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }

    @Test
    @Necessity(false)
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */
        Collector<String, ?, String> toBitString = new Collector<String, List<Integer>, String>() {
            @Override
            public Supplier<List<Integer>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<Integer>, String> accumulator() {
                return (list, str) -> {
                    List<Integer> numbers = Stream.of(str.split("-"))
                            .map(Integer::valueOf).collect(Collectors.toList());
                    if (numbers.size() == 1) {
                        int number = numbers.get(0);
                        while (list.size() < number) {
                            list.add(0);
                        }
                        list.set(number - 1, 1);
                    } else {
                        int min = numbers.get(0);
                        int max = numbers.get(1);
                        while (list.size() < max) {
                            list.add(0);
                        }
                        for (int i = min; i <= max; i++) {
                            list.set(i - 1, 1);
                        }
                    }
                };
            }

            @Override
            public BinaryOperator<List<Integer>> combiner() {
                return null;
            }

            @Override
            public Function<List<Integer>, String> finisher() {
                return container -> container.stream()
                        .map(String::valueOf).collect(Collectors.joining());
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.EMPTY_SET;
            }
        };

        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}
