package com.mobiquity.api;

import lombok.*;

import java.math.BigDecimal;

/**
 * @author Andrei Alekseenko <paintkeeper at gmail.com>
 */
@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Item {
    private final Integer index;
    private final BigDecimal weight;
    @EqualsAndHashCode.Include
    private final BigDecimal price;
}
