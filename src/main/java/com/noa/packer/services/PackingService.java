package com.noa.packer.services;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noa.packer.algorithms.LoadingChain;
import com.noa.packer.models.Container;
import com.noa.packer.models.PackingResult;
import com.noa.packer.repositories.ContainerRepository;
import com.noa.packer.repositories.PackingResultRepository;

@Service
public class PackingService {
    @Autowired
    private PackingResultRepository packingResultRepository;  // ← הזרקה
    @Autowired
    private ContainerRepository containerRepository;  // ← הזרקה

    public ObjectId saveResult(ObjectId userId, LoadingChain chain) {
    List<PackingResult.PlacedBoxData> placements = new ArrayList<>();

    for (LoadingChain.PlacedBoxInfo step : chain.getSteps()) {
        placements.add(new PackingResult.PlacedBoxData(
            step.cargoType,
            step.direction,
            step.leftBackBottom,
            step.rightFrontTop
            // stateKey ו-remainingSpace לא נשמרים
        ));
    }
    Container container = containerRepository.getById(chain.getContainerID());
    PackingResult result = new PackingResult(
    userId,
    chain.getContainerID(),
    container.getVolume(),        // containerVolume
    chain.getTotalCargoVolume(),  // totalCargoVolume
    placements
);

    return packingResultRepository.save(result);
}
}
