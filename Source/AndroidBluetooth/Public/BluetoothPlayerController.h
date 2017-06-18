// Fill out your copyright notice in the Description page of Project Settings.

#pragma once

#include "CoreMinimal.h"
#include "GameFramework/PlayerController.h"
#include "BluetoothPlayerController.generated.h"

/**
 * 
 */
UCLASS()
class ANDROIDBLUETOOTH_API ABluetoothPlayerController : public APlayerController {
	GENERATED_BODY()

public:
    virtual void BeginPlay() override;
	
};
