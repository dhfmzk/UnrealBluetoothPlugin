// Fill out your copyright notice in the Description page of Project Settings.

#include "AndroidBluetoothGameModeBase.h"

void AAndroidBluetoothGameModeBase::ChangeMenuWidget(TSubclassOf<UUserWidget> NewWidgetClass) {
    
    if (CurrentWidget != nullptr) {
        CurrentWidget->RemoveFromViewport();
        CurrentWidget = nullptr;
    }

    if (NewWidgetClass != nullptr) {
        CurrentWidget = CreateWidget<UUserWidget>(GetWorld(), NewWidgetClass);
        if (CurrentWidget != nullptr) {
            CurrentWidget->AddToViewport();
        }
    }
}

void AAndroidBluetoothGameModeBase::BeginPlay() {
}
