import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:poom/widgets/regist/regist_nft_preview.dart';
import 'package:poom/widgets/regist/regist_representive_widget.dart';
import 'package:poom/widgets/regist/regist_specific_info_widget.dart';

class RegistScreen extends StatefulWidget {
  const RegistScreen({Key? key}) : super(key: key);

  @override
  _RegistScreenState createState() => _RegistScreenState();
}

class _RegistScreenState extends State<RegistScreen> {
  File? representImage;

  void _pickRepresentImage() async {
    final pickedFile =
        await ImagePicker().pickImage(source: ImageSource.gallery);
    setState(() {
      if (pickedFile != null) {
        representImage = File(pickedFile.path);
      } else {
        print('No image selected.');
      }
    });
  }

  int _selectedIndex = 0; // 선택된 인덱스

  void nextPage() {
    if (_selectedIndex < 2) {
      setState(() {
        _selectedIndex = _selectedIndex + 1;
      });
    }
  }

  void prevPage() {
    if (_selectedIndex > 0) {
      setState(() {
        _selectedIndex = _selectedIndex - 1;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('보호견 등록'),
        backgroundColor: Theme.of(context).colorScheme.background,
        foregroundColor: const Color(0xFF333333),
        centerTitle: true,
        elevation: 1,
      ),
      body: IndexedStack(
        index: _selectedIndex,
        children: [
          RegistRepresentive(
              nextPage: nextPage,
              pickRepresentImage: () => _pickRepresentImage(),
              representImage: representImage),
          RegistNftPreview(nextPage: nextPage, prevPage: prevPage),
          const RegistSpecificInfo(),
        ],
      ),
    );
  }
}
