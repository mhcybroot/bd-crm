package com.bdcrm.commandcenter;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/command-center")
@RequiredArgsConstructor
public class CommandCenterController {

    private final CommandCenterService commandCenterService;

    @GetMapping
    public CommandCenterResponse current() {
        return commandCenterService.current();
    }
}
