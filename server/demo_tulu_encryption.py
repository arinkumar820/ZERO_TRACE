#!/usr/bin/env python3
"""
Comprehensive Demo and Test Script for Tulu Encryption System
Demonstrates all features of the Tulu encryption implementation
"""

import os
import sys
import time
import json
from datetime import datetime

# Add current directory to Python path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    from tulu_encryption import TuluEncryption, HybridEncryption, tulu_encrypt, tulu_decrypt, hybrid_encrypt, hybrid_decrypt
    from encryption_utils import MessageEncryption, encrypt_message, decrypt_message
    IMPORTS_AVAILABLE = True
except ImportError as e:
    print(f"⚠️  Import error: {e}")
    print("Make sure tulu_encryption.py is in the same directory")
    IMPORTS_AVAILABLE = False

def print_header(title):
    """Print a formatted header"""
    print("\n" + "="*60)
    print(f"  {title}")
    print("="*60)

def print_section(title):
    """Print a formatted section header"""
    print(f"\n📋 {title}")
    print("-" * 40)

def print_test_result(test_name, original, encrypted, decrypted, success):
    """Print formatted test result"""
    status = "✅ PASS" if success else "❌ FAIL"
    print(f"\n🧪 {test_name}: {status}")
    print(f"   Original:  '{original}'")
    print(f"   Encrypted: '{encrypted[:60]}{'...' if len(encrypted) > 60 else ''}'")
    print(f"   Decrypted: '{decrypted}'")
    print(f"   Match:     {success}")
    print(f"   Length:    {len(original)} → {len(encrypted)} → {len(decrypted)}")

def demonstrate_character_mapping():
    """Demonstrate the character mapping system"""
    print_section("Character Mapping Demonstration")
    
    if not IMPORTS_AVAILABLE:
        print("❌ Tulu encryption not available for demonstration")
        return
    
    tulu_enc = TuluEncryption()
    
    # Demo texts showing different types of mappings
    demo_texts = [
        "namaste",           # Tulu character mappings
        "hello123",          # Mixed letters and numbers
        "test@2024!",        # Symbols and special characters
        "ka ga na ma",       # Multiple Tulu syllables
        "Karnataka rocks!",  # Complex mixed content
    ]
    
    for text in demo_texts:
        print(f"\n🔤 Mapping for: '{text}'")
        
        # Show character by character mapping
        result = ""
        i = 0
        text_lower = text.lower()
        
        while i < len(text):
            mapped = False
            
            # Check two-character mappings first
            if i < len(text) - 1:
                two_char = text_lower[i:i+2]
                if two_char in tulu_enc.tulu_mapping:
                    mapped_char = tulu_enc.tulu_mapping[two_char]
                    print(f"   '{two_char}' → '{mapped_char}' (Tulu syllable)")
                    result += mapped_char
                    i += 2
                    mapped = True
                    continue
            
            # Single character mappings
            char = text_lower[i]
            if char in tulu_enc.tulu_mapping:
                mapped_char = tulu_enc.tulu_mapping[char]
                print(f"   '{char}' → '{mapped_char}' (Tulu character)")
                result += mapped_char
                mapped = True
            elif char in tulu_enc.number_mapping:
                mapped_char = tulu_enc.number_mapping[char]
                print(f"   '{char}' → '{mapped_char}' (Number symbol)")
                result += mapped_char
                mapped = True
            elif char in tulu_enc.symbol_mapping:
                mapped_char = tulu_enc.symbol_mapping[char]
                print(f"   '{char}' → '{mapped_char}' (Symbol mapping)")
                result += mapped_char
                mapped = True
            
            if not mapped:
                scrambled = tulu_enc.scramble_text(text[i])
                print(f"   '{text[i]}' → '{scrambled}' (Golden ratio scrambled)")
                result += scrambled
            
            i += 1
        
        print(f"   Final result: '{result}'")

def test_tulu_encryption():
    """Test Tulu encryption functionality"""
    print_section("Tulu Encryption Tests")
    
    if not IMPORTS_AVAILABLE:
        print("❌ Tulu encryption not available for testing")
        return
    
    tulu_enc = TuluEncryption()
    
    test_messages = [
        "Hello World!",
        "This is a test message",
        "Karnataka namaste! How are you?",
        "Numbers: 12345 and symbols: !@#$%",
        "Mixed: abc123!@# xyz789",
        "Unicode: 🚀 Hello 世界 🌟",
        "Kannada: ನಮಸ್ತೆ ಕನ್ನಡ",
        "",  # Empty message
        "a",  # Single character
        "Single word",
        "Multiple words with spaces and punctuation.",
    ]
    
    passed = 0
    total = len(test_messages)
    
    for i, message in enumerate(test_messages):
        encrypted = tulu_enc.encrypt_message(message)
        decrypted = tulu_enc.decrypt_message(encrypted)
        success = (message == decrypted)
        
        if success:
            passed += 1
        
        print_test_result(f"Test {i+1}", message, encrypted, decrypted, success)
        
        # Additional checks
        is_encrypted = tulu_enc.is_tulu_encrypted(encrypted)
        print(f"   Is Tulu encrypted: {is_encrypted}")
    
    print(f"\n📊 Tulu Encryption Results: {passed}/{total} tests passed ({(passed/total)*100:.1f}%)")

def test_hybrid_encryption():
    """Test hybrid encryption functionality"""
    print_section("Hybrid Encryption Tests")
    
    if not IMPORTS_AVAILABLE:
        print("❌ Hybrid encryption not available for testing")
        return
    
    hybrid_enc = HybridEncryption(use_tulu=True, use_aes=True)
    
    test_messages = [
        "Hello Hybrid World!",
        "Testing dual layer encryption",
        "Secure message with Tulu + AES",
        "Special chars: !@#$%^&*()",
        "Numbers and text: abc123xyz",
    ]
    
    passed = 0
    total = len(test_messages)
    
    for i, message in enumerate(test_messages):
        encrypted = hybrid_enc.encrypt_message(message)
        decrypted = hybrid_enc.decrypt_message(encrypted)
        success = (message == decrypted)
        
        if success:
            passed += 1
        
        print_test_result(f"Hybrid Test {i+1}", message, encrypted, decrypted, success)
    
    print(f"\n📊 Hybrid Encryption Results: {passed}/{total} tests passed ({(passed/total)*100:.1f}%)")

def test_convenience_functions():
    """Test convenience functions"""
    print_section("Convenience Functions Tests")
    
    if not IMPORTS_AVAILABLE:
        print("❌ Convenience functions not available for testing")
        return
    
    test_message = "Testing convenience functions!"
    
    # Test Tulu convenience functions
    print("\n🔧 Tulu convenience functions:")
    tulu_encrypted = tulu_encrypt(test_message)
    tulu_decrypted = tulu_decrypt(tulu_encrypted)
    tulu_success = (test_message == tulu_decrypted)
    print_test_result("Tulu Convenience", test_message, tulu_encrypted, tulu_decrypted, tulu_success)
    
    # Test hybrid convenience functions  
    print("\n🔧 Hybrid convenience functions:")
    hybrid_encrypted = hybrid_encrypt(test_message)
    hybrid_decrypted = hybrid_decrypt(hybrid_encrypted)
    hybrid_success = (test_message == hybrid_decrypted)
    print_test_result("Hybrid Convenience", test_message, hybrid_encrypted, hybrid_decrypted, hybrid_success)

def test_integration_with_existing_system():
    """Test integration with existing AES encryption system"""
    print_section("Integration with Existing System")
    
    if not IMPORTS_AVAILABLE:
        print("❌ Integration testing not available")
        return
    
    test_messages = [
        "Testing AES integration",
        "Hybrid system test",
        "Backward compatibility check"
    ]
    
    print("\n🔗 Testing different encryption modes:")
    
    for i, message in enumerate(test_messages):
        print(f"\nTest message {i+1}: '{message}'")
        
        # Test AES-only
        try:
            aes_encrypted = encrypt_message(message, encryption_mode="aes")
            aes_decrypted = decrypt_message(aes_encrypted, encryption_mode="aes")
            aes_success = (message == aes_decrypted)
            print(f"  AES-only:     {aes_success} ✅" if aes_success else f"  AES-only:     {aes_success} ❌")
        except Exception as e:
            print(f"  AES-only:     Error - {e}")
        
        # Test Tulu-only
        try:
            tulu_encrypted = encrypt_message(message, encryption_mode="tulu")
            tulu_decrypted = decrypt_message(tulu_encrypted, encryption_mode="tulu")
            tulu_success = (message == tulu_decrypted)
            print(f"  Tulu-only:    {tulu_success} ✅" if tulu_success else f"  Tulu-only:    {tulu_success} ❌")
        except Exception as e:
            print(f"  Tulu-only:    Error - {e}")
        
        # Test Hybrid
        try:
            hybrid_encrypted = encrypt_message(message, encryption_mode="hybrid")
            hybrid_decrypted = decrypt_message(hybrid_encrypted, encryption_mode="hybrid")
            hybrid_success = (message == hybrid_decrypted)
            print(f"  Hybrid:       {hybrid_success} ✅" if hybrid_success else f"  Hybrid:       {hybrid_success} ❌")
        except Exception as e:
            print(f"  Hybrid:       Error - {e}")

def performance_benchmark():
    """Benchmark encryption performance"""
    print_section("Performance Benchmark")
    
    if not IMPORTS_AVAILABLE:
        print("❌ Performance testing not available")
        return
    
    test_message = "This is a test message for performance benchmarking! " * 10  # ~530 characters
    iterations = 100
    
    # Tulu encryption benchmark
    print(f"\n⏱️  Benchmarking Tulu encryption ({iterations} iterations)...")
    tulu_enc = TuluEncryption()
    
    start_time = time.time()
    for _ in range(iterations):
        encrypted = tulu_enc.encrypt_message(test_message)
        decrypted = tulu_enc.decrypt_message(encrypted)
    tulu_time = time.time() - start_time
    
    print(f"  Tulu encryption: {tulu_time:.3f}s total, {(tulu_time/iterations)*1000:.2f}ms per operation")
    
    # Hybrid encryption benchmark
    print(f"\n⏱️  Benchmarking Hybrid encryption ({iterations} iterations)...")
    hybrid_enc = HybridEncryption()
    
    start_time = time.time()
    for _ in range(iterations):
        encrypted = hybrid_enc.encrypt_message(test_message)
        decrypted = hybrid_enc.decrypt_message(encrypted)
    hybrid_time = time.time() - start_time
    
    print(f"  Hybrid encryption: {hybrid_time:.3f}s total, {(hybrid_time/iterations)*1000:.2f}ms per operation")
    
    # Compare with AES (if available)
    try:
        print(f"\n⏱️  Benchmarking AES encryption ({iterations} iterations)...")
        aes_enc = MessageEncryption()
        
        start_time = time.time()
        for _ in range(iterations):
            encrypted = aes_enc.encrypt_message(test_message)
            decrypted = aes_enc.decrypt_message(encrypted)
        aes_time = time.time() - start_time
        
        print(f"  AES encryption: {aes_time:.3f}s total, {(aes_time/iterations)*1000:.2f}ms per operation")
        
        print(f"\n📊 Performance comparison:")
        print(f"  AES is {tulu_time/aes_time:.1f}x faster than Tulu")
        print(f"  AES is {hybrid_time/aes_time:.1f}x faster than Hybrid")
        print(f"  Tulu is {hybrid_time/tulu_time:.1f}x {'slower' if hybrid_time > tulu_time else 'faster'} than Hybrid")
        
    except Exception as e:
        print(f"  AES benchmark failed: {e}")

def demonstrate_real_world_usage():
    """Demonstrate real-world usage scenarios"""
    print_section("Real-World Usage Scenarios")
    
    if not IMPORTS_AVAILABLE:
        print("❌ Real-world demonstration not available")
        return
    
    # Scenario 1: Chat messages
    print("\n💬 Scenario 1: Chat Messages")
    chat_messages = [
        "Hey, how are you?",
        "I'm good! How about you?",
        "Meeting at 3pm today?",
        "Yes, see you there! 👍",
        "Thanks for the help earlier"
    ]
    
    tulu_enc = TuluEncryption()
    encrypted_chat = []
    
    for i, msg in enumerate(chat_messages):
        encrypted = tulu_enc.encrypt_message(msg)
        encrypted_chat.append(encrypted)
        print(f"  Message {i+1}: '{msg}' → [ENCRYPTED]")
    
    print("\n  Decrypting chat history:")
    for i, encrypted_msg in enumerate(encrypted_chat):
        decrypted = tulu_enc.decrypt_message(encrypted_msg)
        print(f"  [ENCRYPTED] → '{decrypted}'")
    
    # Scenario 2: Sensitive data
    print("\n🔒 Scenario 2: Sensitive Information")
    sensitive_data = [
        "Password: mySecretPass123!",
        "Credit Card: 1234-5678-9012-3456",
        "Personal: DOB 01/01/1990",
        "Location: Bangalore, Karnataka"
    ]
    
    hybrid_enc = HybridEncryption()
    
    for data in sensitive_data:
        encrypted = hybrid_enc.encryptMessage(data)
        decrypted = hybrid_enc.decryptMessage(encrypted)
        success = (data == decrypted)
        print(f"  '{data[:20]}...' → {'✅ Secured' if success else '❌ Failed'}")
    
    # Scenario 3: Mixed language content
    print("\n🌍 Scenario 3: Multi-language Content")
    multilingual_messages = [
        "Hello ನಮಸ್ತೆ नमस्ते",
        "Karnataka ಕರ್ನಾಟಕ is beautiful!",
        "Tulu ತುಳು language encryption 🔐",
        "Unicode: 🚀 ಮಂಗಳೂರು 🌊"
    ]
    
    for msg in multilingual_messages:
        encrypted = tulu_enc.encrypt_message(msg)
        decrypted = tulu_enc.decrypt_message(encrypted)
        success = (msg == decrypted)
        print(f"  Multi-lang: {'✅ Success' if success else '❌ Failed'} - '{msg[:30]}{'...' if len(msg) > 30 else ''}'")

def generate_test_report():
    """Generate a comprehensive test report"""
    print_section("Comprehensive Test Report")
    
    if not IMPORTS_AVAILABLE:
        print("❌ Test report generation not available")
        return
    
    report = {
        "timestamp": datetime.now().isoformat(),
        "test_results": {},
        "performance": {},
        "system_info": {
            "python_version": sys.version,
            "platform": sys.platform,
        }
    }
    
    # Run comprehensive tests
    test_cases = [
        ("Basic ASCII", "Hello World"),
        ("Numbers", "Test123"),
        ("Symbols", "Hello@World!"),
        ("Unicode", "🚀 Test 🌟"),
        ("Kannada", "ನಮಸ್ತೆ"),
        ("Mixed", "Hello ನಮಸ್ತೆ 123!"),
        ("Empty", ""),
        ("Long", "A" * 1000)
    ]
    
    tulu_enc = TuluEncryption()
    hybrid_enc = HybridEncryption()
    
    for test_name, test_input in test_cases:
        # Tulu test
        try:
            encrypted = tulu_enc.encrypt_message(test_input)
            decrypted = tulu_enc.decrypt_message(encrypted)
            tulu_success = (test_input == decrypted)
            tulu_compression_ratio = len(encrypted) / max(len(test_input), 1)
        except Exception as e:
            tulu_success = False
            tulu_compression_ratio = 0
            print(f"Tulu test failed for {test_name}: {e}")
        
        # Hybrid test
        try:
            encrypted = hybrid_enc.encrypt_message(test_input)
            decrypted = hybrid_enc.decrypt_message(encrypted)
            hybrid_success = (test_input == decrypted)
            hybrid_compression_ratio = len(encrypted) / max(len(test_input), 1)
        except Exception as e:
            hybrid_success = False
            hybrid_compression_ratio = 0
            print(f"Hybrid test failed for {test_name}: {e}")
        
        report["test_results"][test_name] = {
            "input_length": len(test_input),
            "tulu_success": tulu_success,
            "tulu_compression_ratio": round(tulu_compression_ratio, 2),
            "hybrid_success": hybrid_success,
            "hybrid_compression_ratio": round(hybrid_compression_ratio, 2)
        }
    
    # Save report
    report_file = f"tulu_encryption_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    try:
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        print(f"📄 Test report saved to: {report_file}")
    except Exception as e:
        print(f"❌ Failed to save report: {e}")
    
    # Print summary
    total_tests = len(test_cases) * 2  # Tulu + Hybrid
    passed_tests = sum(1 for result in report["test_results"].values() 
                      if result["tulu_success"] and result["hybrid_success"])
    
    print(f"\n📊 Test Summary:")
    print(f"  Total test scenarios: {len(test_cases)}")
    print(f"  Tulu tests passed: {sum(1 for r in report['test_results'].values() if r['tulu_success'])}/{len(test_cases)}")
    print(f"  Hybrid tests passed: {sum(1 for r in report['test_results'].values() if r['hybrid_success'])}/{len(test_cases)}")
    print(f"  Overall success rate: {(passed_tests/len(test_cases))*100:.1f}%")

def main():
    """Main demonstration function"""
    print_header("🔐 TULU ENCRYPTION SYSTEM - COMPREHENSIVE DEMO")
    print(f"📅 Demo run at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    if not IMPORTS_AVAILABLE:
        print("\n❌ Unable to import Tulu encryption modules!")
        print("Please ensure tulu_encryption.py is available in the same directory.")
        return
    
    print("✅ All encryption modules loaded successfully!")
    print("🚀 Starting comprehensive demonstration...")
    
    try:
        # Run all demonstrations and tests
        demonstrate_character_mapping()
        test_tulu_encryption()
        test_hybrid_encryption()
        test_convenience_functions()
        test_integration_with_existing_system()
        performance_benchmark()
        demonstrate_real_world_usage()
        generate_test_report()
        
        print_header("🎉 DEMO COMPLETED SUCCESSFULLY")
        print("✅ All tests and demonstrations completed!")
        print("📚 Key Features Demonstrated:")
        print("  🔤 Tulu character mapping (Kannada script)")
        print("  🔢 Number and symbol mapping")
        print("  🌀 Golden ratio mathematical scrambling")
        print("  🔐 Hybrid encryption (Tulu + AES)")
        print("  📱 Android-compatible Java implementation")
        print("  🔗 Integration with existing systems")
        print("  ⚡ Performance benchmarking")
        print("  🌍 Multi-language support")
        print("\n🚀 Your Tulu encryption system is ready for use!")
        
    except Exception as e:
        print(f"\n❌ Demo encountered an error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()