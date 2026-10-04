package pl.hubmalopolski.hub.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import pl.hubmalopolski.hub.domain.Region;

@Component
public class RegionConverter implements Converter<String, Region> {
    @Override
    public Region convert(String source) {
        return Region.fromCode(source);
    }
}
