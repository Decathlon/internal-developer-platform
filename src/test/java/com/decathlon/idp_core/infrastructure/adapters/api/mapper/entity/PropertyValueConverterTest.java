package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Constructor;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

class PropertyValueConverterTest {

  @Test
  void preservesValueWhenDefinitionIsNull() {
    var value = "legacy value";
    var property = new Property(null, "label", value);

    assertThat(PropertyValueConverter.convert(property, null)).isSameAs(value);
  }

  @Test
  void preservesArrayValue() {
    var value = new boolean[]{true, false};
    var property = new Property(null, "flags", value);
    var definition = definition("flags", PropertyType.BOOLEAN);

    assertThat(PropertyValueConverter.convert(property, definition)).isSameAs(value);
  }

  @Test
  void preservesCollectionValue() {
    var value = List.of(true, false);
    var property = new Property(null, "flags", value);
    var definition = definition("flags", PropertyType.BOOLEAN);

    assertThat(PropertyValueConverter.convert(property, definition)).isSameAs(value);
  }

  @Test
  void preservesNativeNumericValue() {
    var property = new Property(null, "port", 8080);
    var definition = definition("port", PropertyType.NUMBER);

    assertThat(PropertyValueConverter.convert(property, definition)).isSameAs(property.value());
  }

  @Test
  void convertsNumericStringToDouble() {
    var property = new Property(null, "port", "8080.5");
    var definition = definition("port", PropertyType.NUMBER);

    assertThat(PropertyValueConverter.convert(property, definition)).isEqualTo(8080.5d);
  }

  @Test
  void preservesInvalidNumericString() {
    var property = new Property(null, "port", "not-a-number");
    var definition = definition("port", PropertyType.NUMBER);

    assertThat(PropertyValueConverter.convert(property, definition)).isSameAs(property.value());
  }

  @Test
  void preservesNativeBooleanValue() {
    var property = new Property(null, "enabled", true);
    var definition = definition("enabled", PropertyType.BOOLEAN);

    assertThat(PropertyValueConverter.convert(property, definition)).isSameAs(property.value());
  }

  @Test
  void convertsBooleanStringsToBoolean() {
    var definition = definition("enabled", PropertyType.BOOLEAN);

    assertThat(PropertyValueConverter.convert(new Property(null, "enabled", "TRUE"), definition))
        .isEqualTo(true);
    assertThat(PropertyValueConverter.convert(new Property(null, "enabled", "false"), definition))
        .isEqualTo(false);
  }

  @Test
  void preservesInvalidBooleanString() {
    var property = new Property(null, "enabled", "yes");
    var definition = definition("enabled", PropertyType.BOOLEAN);

    assertThat(PropertyValueConverter.convert(property, definition)).isSameAs(property.value());
  }

  @Test
  void preservesValueForStringDefinition() {
    var property = new Property(null, "label", "value");
    var definition = definition("label", PropertyType.STRING);

    assertThat(PropertyValueConverter.convert(property, definition)).isSameAs(property.value());
  }

  @Test
  void hasPrivateConstructor() throws Exception {
    Constructor<PropertyValueConverter> constructor = PropertyValueConverter.class
        .getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThatCode(constructor::newInstance).doesNotThrowAnyException();
  }

  private static PropertyDefinition definition(String name, PropertyType type) {
    return new PropertyDefinition(null, name, name, type, false, null);
  }
}
