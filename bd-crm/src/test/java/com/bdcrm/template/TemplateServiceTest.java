package com.bdcrm.template;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bdcrm.common.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemplateServiceTest {

    @Test
    void rejectsTemplatesWithMoreThanSevenSteps() {
        TemplateService service = new TemplateService(null);

        List<TemplateStepRequest> steps = java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(step -> new TemplateStepRequest(step, step, ContactChannel.CALL, "Step " + step))
                .toList();

        assertThatThrownBy(() -> service.validateSteps(steps))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("at most 7");
    }
}
