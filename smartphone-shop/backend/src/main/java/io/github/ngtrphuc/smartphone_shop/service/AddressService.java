package io.github.ngtrphuc.smartphone_shop.service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.model.Address;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.AddressRepository;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;

@Service
public class AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 10;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-()\\s]{6,30}$");

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Address> listUserAddresses(String email) {
        User user = findUserByEmail(email);
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());
    }

    @Transactional
    public Address createAddress(String email, AddressCommand command) {
        User user = findUserByEmail(email);
        long existingCount = addressRepository.countByUserId(user.getId());
        if (existingCount >= MAX_ADDRESSES_PER_USER) {
            throw new ValidationException("You can save up to 10 addresses.");
        }

        Address address = new Address();
        address.setUser(user);
        applyCommand(address, command);

        boolean hasDefault = addressRepository.findFirstByUserIdAndIsDefaultTrueOrderByCreatedAtDesc(user.getId()).isPresent();
        boolean shouldBeDefault = command.isDefault() || !hasDefault;
        if (shouldBeDefault) {
            addressRepository.clearDefaultForUser(user.getId());
            address.setDefault(true);
        }

        Address saved = requireSavedAddress(addressRepository.save(Objects.requireNonNull(address)));
        syncUserDefaultAddress(user);
        return saved;
    }

    @Transactional
    public Address updateAddress(String email, Long addressId, AddressCommand command) {
        User user = findUserByEmail(email);
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ValidationException("Address not found."));

        applyCommand(address, command);
        if (command.isDefault()) {
            addressRepository.clearDefaultForUser(user.getId());
            address.setDefault(true);
        }

        Address saved = requireSavedAddress(addressRepository.save(Objects.requireNonNull(address)));
        syncUserDefaultAddress(user);
        return saved;
    }

    @Transactional
    public void deleteAddress(String email, Long addressId) {
        User user = findUserByEmail(email);
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ValidationException("Address not found."));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());
            if (!remaining.isEmpty()) {
                Address promote = remaining.getFirst();
                addressRepository.clearDefaultForUser(user.getId());
                promote.setDefault(true);
                requireSavedAddress(addressRepository.save(Objects.requireNonNull(promote)));
            }
        }
        syncUserDefaultAddress(user);
    }

    @Transactional
    public Address setDefaultAddress(String email, Long addressId) {
        User user = findUserByEmail(email);
        Address target = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ValidationException("Address not found."));

        addressRepository.clearDefaultForUser(user.getId());
        target.setDefault(true);
        Address saved = requireSavedAddress(addressRepository.save(Objects.requireNonNull(target)));
        syncUserDefaultAddress(user);
        return saved;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ValidationException("User not found."));
    }

    private void applyCommand(Address address, AddressCommand command) {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(command, "command");

        String recipientName = normalizeRequired(command.recipientName(), "Recipient name is required.", 100);
        String streetAddress = normalizeRequired(command.streetAddress(), "Street address is required.", 200);
        String label = normalizeOptional(command.label(), 50);
        String phoneNumber = normalizeOptional(command.phoneNumber(), 20);
        String postalCode = normalizeOptional(command.postalCode(), 10);
        String prefecture = normalizeOptional(command.prefecture(), 30);
        String city = normalizeOptional(command.city(), 50);
        String building = normalizeOptional(command.building(), 100);

        if (phoneNumber != null && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new ValidationException("Phone number format is invalid.");
        }

        address.setLabel(label);
        address.setRecipientName(recipientName);
        address.setPhoneNumber(phoneNumber);
        address.setPostalCode(postalCode);
        address.setPrefecture(prefecture);
        address.setCity(city);
        address.setStreetAddress(streetAddress);
        address.setBuilding(building);
    }

    private void syncUserDefaultAddress(User user) {
        Address defaultAddress = addressRepository.findFirstByUserIdAndIsDefaultTrueOrderByCreatedAtDesc(user.getId())
                .orElse(null);
        String fullAddress = defaultAddress == null ? null : defaultAddress.toFullAddress();
        user.setDefaultAddress(fullAddress);
        userRepository.save(user);
    }

    private @NonNull Address requireSavedAddress(@Nullable Address saved) {
        return Objects.requireNonNull(saved, "Saved address must not be null.");
    }

    private String normalizeRequired(String value, String emptyMessage, int maxLength) {
        String normalized = normalizeOptional(value, maxLength);
        if (normalized == null) {
            throw new ValidationException(emptyMessage);
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new ValidationException("Field is too long.");
        }
        return normalized;
    }

    public record AddressCommand(
            String label,
            String recipientName,
            String phoneNumber,
            String postalCode,
            String prefecture,
            String city,
            String streetAddress,
            String building,
            boolean isDefault) {
    }
}
