/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.core.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.jboss.pnc.rex.core.config.api.MutinyRetryPolicy;

public class ValidBackoffValidator implements ConstraintValidator<ValidBackoff, MutinyRetryPolicy.ExpBackoff> {

    private String message;

    @Override
    public void initialize(ValidBackoff constraintAnnotation) {
        message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(MutinyRetryPolicy.ExpBackoff value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        var initial = value.initialDelay();
        var max = value.maxDelay();

        // if max delay is non-zero, initial delay must also be non-zero
        if (!max.isZero() && initial.isZero()) {
            context.buildConstraintViolationWithTemplate(message);
            return false;
        }

        return true;
    }
}
