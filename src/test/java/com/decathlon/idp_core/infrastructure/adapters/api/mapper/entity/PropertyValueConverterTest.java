package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

class PropertyValueConverterTest {

  @Test
  void preservesNativeBooleanArray() {
    var value = List.of(true, false);
    var property = new Property(null, "flags", value);
    var definition = new PropertyDefinition(null, "flags", "Flags", PropertyType.BOOLEAN, false,
        null);

    assertThat(PropertyValueConverter.convert(property, definition)).isEqualTo(value);
  }

  @Test
  void preservesNativeNumericValue() {
    var property = new Property(null, "port", 8080);
    var definition = new PropertyDefinition(null, "port", "Port", PropertyType.NUMBER, false, null);

    assertThat(PropertyValueConverter.convert(property, definition)).isEqualTo(8080);
  }
}
