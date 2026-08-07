package org.cloudfoundry.multiapps.controller.client.facade;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an Immutables collection attribute (typically a {@code Map}) as permitting {@code null} values. Project-owned replacement for
 * {@code org.cloudfoundry.AllowNulls} (from cf-java-client) so the domain model no longer depends on the OSS client. Immutables
 * recognises this marker by its simple name {@code AllowNulls}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface AllowNulls {
}
