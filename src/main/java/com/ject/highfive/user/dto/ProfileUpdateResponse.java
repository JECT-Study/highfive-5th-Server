package com.ject.highfive.user.dto;

import com.ject.highfive.user.entity.UserStatus;

public record ProfileUpdateResponse(UserStatus status, NextStep nextStep) {}
