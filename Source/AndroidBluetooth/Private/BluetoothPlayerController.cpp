// Fill out your copyright notice in the Description page of Project Settings.

#include "BluetoothPlayerController.h"

void ABluetoothPlayerController::BeginPlay() {
    Super::BeginPlay();
    SetInputMode(FInputModeGameAndUI());
}


